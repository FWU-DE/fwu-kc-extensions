$baseUrl="http://localhost:18080/auth"
$realm="fwu"
$clientId="applicy"
$clientSecret=""
$username="iam-admin"
$password="test"
$values='"brock","misty","iam-admin"'
$testValue="49060eab-7c61-3989-9da2-33a680255961"

$tokenRequest=@{
    grant_type='password'
    username=$username
    password=$password
    client_id=$clientId
    $clientSecret=$clientSecret
}
$tokenResponse = Invoke-RestMethod -Method 'POST' -Uri "${baseUrl}/realms/${realm}/protocol/openid-connect/token" -ContentType 'application/x-www-form-urlencoded' -Body $tokenRequest
$token=$tokenResponse.access_token

$hmacRequest=@{
    clientId=$clientId
    originalValues=[$values]
    testValue=$testValue
}
$hmacHeaders=@{
    Authorization="Bearer $token"
}
Invoke-RestMethod -Method 'POST' -Uri "${baseUrl}/realms/${realm}/hmac" -Headers $hmacHeaders -ContentType 'application/json' -Body $hmacRequest
