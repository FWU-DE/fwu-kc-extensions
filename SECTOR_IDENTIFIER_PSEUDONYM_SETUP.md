# Sector identifier pseudonym pass-through setup guide

## Goal

Some IdPs want to compute the pseudonymized `sub` (and/or pseudonymized email) for a user themselves, instead of
letting Keycloak's `HmacPairwiseSubMapper` / `HmacPairwiseEmailMapper` do it. To group users the same way Keycloak
would, the IdP needs to know the client's `sectorIdentifierUri`. Once it knows this, it can compute its own pseudonym
and send it back to Keycloak in a claim, which Keycloak then uses as-is instead of computing its own HMAC hash.

This involves components from two modules:

* [`acr-values-authenticator`](acr-values-authenticator/README.md) - authenticators that forward the
  `sectorIdentifierUri` to the IdP and (optionally) verify it comes back unchanged
* [`hmac-mapper`](hmac-mapper/README.md) - the `HmacPairwiseSubMapper` / `HmacPairwiseEmailMapper` mappers that can
  use an IdP-provided pseudonym instead of computing one

The flow looks like this:

```
Keycloak                                  IdP
--------                                  ---
1. SectorIdentifierIdpValuesForwarderAuth
   adds ?sector_identifier_uri=... to
   the redirect  ─────────────────────────▶
                                           2. IdP receives sectorIdentifierUri,
                                              computes its own pseudonym for
                                              the user, and returns both the
                                              sectorIdentifierUri (echoed back)
                                              and the pseudonym as claims
   ◀─────────────────────────────────────
3. IdP mappers write both claims to
   user attributes
4. SectorIdentifierVerifierAuthenticator
   (optional) checks the echoed
   sectorIdentifierUri attribute matches
   what was sent in step 1
5. HmacPairwiseSubMapper / -EmailMapper
   use the pseudonym attribute directly
   as sub / email base, instead of
   computing one via HMAC
```

### Prerequisite

The client must already have a `HmacPairwiseSubMapper` (and/or `HmacPairwiseEmailMapper`) configured with a
`sectorIdentifierUri` - see [hmac-mapper/README.md](hmac-mapper/README.md#configuration). This guide only adds to
that existing configuration.

## Step 1: Add `SectorIdentifierIdpValuesForwarderAuth` to the browser flow

1. Go to **Authentication** and select (or duplicate) the flow used as browser flow.
2. Click **Add step** and search for **"Sector identifier URI IDP values params appender"**.
3. Add it as a step with requirement **REQUIRED** (it never denies login - see "Behaviour" below - so REQUIRED is
   safe even for clients without a HMAC mapper configured).
4. Configure it:
   * **Sector identifier URI param name**: the name of the request parameter sent to the IdP, default
     `sector_identifier_uri`. Note this value, it is needed again in step 2.
5. Bind this flow as the browser flow (**Action** dropdown → **Bind flow** → select **Browser flow**).

See [acr-values-authenticator/README.md](acr-values-authenticator/README.md#sector-identifier-uri-idp-values-forwarder-authenticator)
for more details.

## Step 2: Forward the request parameter on the IdP

1. Go to **Identity providers** and select the IdP that should receive the sector identifier.
2. Open **Advanced settings**.
3. Add the param name configured in step 1 (e.g. `sector_identifier_uri`) to **Forwarded query parameters**. If other
   parameters are already forwarded, separate them with a comma.

The IdP will now receive `sector_identifier_uri` as a query parameter on the authentication request and is expected
to send back, in the response, the (echoed) sector identifier URI and the pseudonym it computed - typically as
custom claims in the ID token, access token, or userinfo response, depending on what the IdP supports.

## Step 3: Map the IdP's claims to user attributes

On the same identity provider, add two mappers (type depends on the IdP's protocol, e.g. "User Attribute" for OIDC or
"Attribute Importer" for SAML):

1. **Sector identifier URI mapper**: maps the claim containing the echoed sector identifier URI to a user attribute,
   e.g. `vidis_sector_identifier_uri` (this is the default expected by `SectorIdentifierVerifierAuthenticator`, see
   step 4).
2. **Pseudonym mapper**: maps the claim containing the IdP-computed pseudonym to a user attribute of your choice, e.g.
   `idp_pseudonym_sub`. This attribute name is needed again in step 5.

## Step 4 (recommended): Verify the returned sector identifier URI

To make sure the value that comes back from the IdP was not tampered with, add the post-login verification
authenticator:

1. Select the authentication flow configured as the **post login flow** for this IdP (or create one).
2. Add step **"Sector identifier URI verifier"** as a **REQUIRED** step.
3. Configure **User attribute name** to match the attribute used in step 3.1 (or leave blank to use the default
   `vidis_sector_identifier_uri`).

If no sector identifier URI was sent to the IdP in the first place (e.g. the client has no HMAC mapper configured),
this authenticator does nothing and just allows the flow to continue. The same applies if the IdP echoes back
neither the sector identifier URI nor a pseudonym (i.e. it also ignored the sector identifier itself) - Keycloak
then falls back to generating its own pseudonym as before. If a pseudonym *was* sent back but the sector identifier
URI does not match, access is denied, since that combination is inconsistent.

See [acr-values-authenticator/README.md](acr-values-authenticator/README.md#sector-identifier-uri-verifier-authenticator)
for more details.

## Step 5: Configure the mappers to use the IdP-provided pseudonym

For each of `HmacPairwiseSubMapper` (claim `sub`) and `HmacPairwiseEmailMapper` (claim `email`) configured on the
client:

1. Open the mapper's configuration.
2. Set **External sub attribute** to the user attribute name configured in step 3.2 (e.g. `idp_pseudonym_sub`).
3. Save.

From now on, whenever a user has a non-blank value for that attribute, the mapper uses it directly as the `sub` (or
as the basis for the generated email) instead of computing it via HMAC. If the attribute is left blank, not
configured, or the user has no value for it (e.g. login via a different IdP, or direct login), the mapper falls back
to the normal HMAC-based computation - existing configurations that don't set this attribute keep working exactly as
before.

See [hmac-mapper/README.md](hmac-mapper/README.md#how-its-working) for more details.

## Summary of configuration names

| # | Component | Config field | Default | Configured in |
|---|---|---|---|---|
| 1 | `SectorIdentifierIdpValuesForwarderAuth` | Sector identifier URI param name | `sector_identifier_uri` | Browser flow step |
| 2 | Identity provider | Forwarded query parameters | - | IdP → Advanced settings |
| 3 | Identity provider mapper | Target user attribute (sector identifier URI) | - | IdP → Mappers |
| 4 | Identity provider mapper | Target user attribute (pseudonym) | - | IdP → Mappers |
| 5 | `SectorIdentifierVerifierAuthenticator` | User attribute name | `vidis_sector_identifier_uri` | Post-login flow step |
| 6 | `HmacPairwiseSubMapper` / `HmacPairwiseEmailMapper` | External sub attribute | *(blank)* | Client → Mapper |

These must be consistent for the flow to work end-to-end:

* Row 1's param name must be the value added to row 2's forwarded query parameters.
* Row 3's target attribute name must match row 5's user attribute name.
* Row 4's target attribute name must match row 6's external sub attribute.
