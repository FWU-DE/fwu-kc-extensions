<#import "template.ftl" as layout>
<@layout.registrationLayout displayMessage=!messagesPerField.existsError('username') displayInfo=(realm.password && realm.registrationAllowed && !registrationDisabled??); section>
    <#if section = "show-username">
        <div class="${properties.kcAccountLinkingInstructionClass!}" style="margin-bottom: 24px;">${kcSanitize(msg("accountLinkingInstruction", idpAlias))?no_esc}</div>
    <#elseif section = "form">
        <div id="kc-form" class="${properties.kcAccountLinkingClass!}">
            <div id="kc-form-wrapper">
                <#if realm.password>
                    <form id="kc-form-login" onsubmit="login.disabled = true; return true;" action="${url.loginAction}"
                          method="post">
                        <div class="${properties.kcFormGroupClass!}">
                            <label for="username"
                                   class="${properties.kcLabelClass!}"><#if !realm.loginWithEmailAllowed>${msg("username")}<#elseif !realm.registrationEmailAsUsername>${msg("usernameOrEmail")}<#else>${msg("email")}</#if></label>

                            <input tabindex="1" id="username"
                                   aria-invalid="<#if messagesPerField.existsError('username')>true</#if>"
                                   class="${properties.kcInputClass!}" name="username"
                                   type="text" autofocus autocomplete="off" value="${username_attr!}"/>

                            <#if messagesPerField.existsError('username')>
                                <span id="input-error-username" class="${properties.kcInputErrorMessageClass!}" aria-live="polite">
                                    ${kcSanitize(messagesPerField.get('username'))?no_esc}
                                </span>
                            </#if>
                        </div>

                        <div id="kc-form-buttons" class="${properties.kcFormGroupClass!}">
                            <div class="${properties.kcFormButtonsWrapperClass!}">
                                <input tabindex="4"
                                       class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                       name="login" id="kc-login" type="submit" value="${msg("doLogIn")}" style="margin-bottom: 8px;"/>
                                <input tabindex="5"
                                       class="${properties.kcButtonClass!} ${properties.kcButtonPrimaryClass!} ${properties.kcButtonBlockClass!} ${properties.kcButtonLargeClass!}"
                                       name="cancel" id="kc-cancel" type="submit" value="${msg("doSkipAccountLinking")}"/>
                            </div>
                        </div>
                    </form>
                </#if>
            </div>
        </div>
    </#if>

</@layout.registrationLayout>
