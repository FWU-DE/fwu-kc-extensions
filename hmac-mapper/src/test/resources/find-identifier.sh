#!/bin/bash

baseUrl="http://localhost:18080/auth"
realm="fwu"
clientId="applicy"
clientSecret=""
username="iam-admin"
password="test"
token=""
values='"brock","misty","iam-admin"'
testValue="49060eab-7c61-3989-9da2-33a680255961"

token=$(curl --location --request POST "${baseUrl}/realms/${realm}/protocol/openid-connect/token" \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode "username=${username}" \
--data-urlencode "password=${password}" \
--data-urlencode "client_id=${clientId}" \
--data-urlencode "client_secret=${clientSecret}" | jq --raw-output ".access_token")
echo $token

curl --location --request POST "${baseUrl}/realms/${realm}/hmac" \
--header "Authorization: Bearer $token" \
--header 'Content-Type: application/json' \
--data-raw "{
    \"clientId\": \"${clientId}\",
    \"originalValues\": [ ${values} ],
    \"testValue\": \"${testValue}\"
}"
