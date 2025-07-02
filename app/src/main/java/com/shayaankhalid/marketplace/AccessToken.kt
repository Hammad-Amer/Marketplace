package com.shayaankhalid.marketplace


import com.google.auth.oauth2.GoogleCredentials
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets

object AccessToken {

    private val firebaseMessagingScope = "https://www.googleapis.com/auth/firebase.messaging"

    fun getAccessToken():String?{

        try{
            val jasonString: String = "{\n" +
                    "  \"type\": \"service_account\",\n" +
                    "  \"project_id\": \"marketplace-7143e\",\n" +
                    "  \"private_key_id\": \"98e3e4db61fee4dba40889eb6edc87b7ecf4b886\",\n" +
                    "  \"private_key\": \"-----BEGIN PRIVATE KEY-----\\nMIIEvAIBADANBgkqhkiG9w0BAQEFAASCBKYwggSiAgEAAoIBAQCazuEfROdOYGeF\\nzAI4oqh3hjVtCKgDaz6qsPyWsQV34GaU+EoQUIrr9Je4M05+sggBh6JOpQQH9LQr\\nJGXvW46fAh+/RfhSPPrmyBesxQN85+Pw7legN/6Fc46U0SLtRaY667SDs1XpGXTd\\nX+slv0I76u/PplF/ngasi1H21lGHPHRSdtMeAE6nl32BguOZKcidarGnuPlCBAyG\\nfi15uPhFPjkbPYZdi7nuHOo+UzZUqeM7t7KQZPv0uIpmvnOpRYBno8jv/pEiA0xc\\ndP4+g+/WK3Mh4qowCCrzXTOTBSi/y+pRRbXqaCCc54wEo2ZLqFOITNdI7LxD6/wu\\n2nLfM3nfAgMBAAECggEAD6bO9/xdbn0sukhQCS3Y3ozBJg7z/bSAF33ha9Ll/KLB\\nfFpGErQx9305QbReQUz6pjzyWgUxDSTeemYpsDOPjeAHGa2XV6k2rNKaIiuhGKXR\\nU/ClB1vh7dgrGDy0zUx3bAmcrJwbGYArYp5qBdz6c1hjepkUMFTn09YaKVytzYBU\\n+Q+JJ81Rg2WWca3p0Rm32Y5qia5MqDn/Tjbuh1nHd1AJMepojEmToLJ1Nymb+dDp\\n+OXwVif8Vs65PCXAQW7J21mc67Bbksx1ngFeJlon0OJH4EQZrkbk8jpKvlbnTm9r\\nZqIAJQLoSBzEC2T6icmiyU7nXmluzr50heaZzMgmwQKBgQDH+y+LoGUzfEXvmFT/\\np92GxW6RAHjzhgfdBtRvFECr6wEY7/995GclThCutqcYISpxji08Xm3La0iOQXmL\\nR4KMtrLdXX0Hok+mERc05IgvNQNSF1CD7JYYLQUicjisxEASbAG5P38VYlSTaalq\\naxa0G3PCdS9D2IYWufSjXCEVrwKBgQDGLEvArX7ttL3lGex2BgZKN48r2ppqtSjx\\nmJzPiilzLYSVAUyelaa1dHajeX/zuV/s7tauSl7yJfK597ffN9cR5h/QPsWQfGC4\\nqsIn8UTYPAE7cmysxz+RYRIZS2OQQNSjqHnkcU1yWL8qDYfHiO+3kxv44Z40pU2B\\nw0jiUBMa0QKBgEZqJ5+eZoPuaiGnHaa3UMU3l2rvI6d7tiEZWouSMgNWyBgjyW5m\\nkfsMtcsO/V79rpg1W7A5/6b0q6OLYmSsVUd+vRQ86t1ZRXpP4j/5iAo3PGgXVX5e\\nO6tfjsNGPLZ4k6bo6OJ1fSXmbalKNyEFfAUG9YevA2l8QBqYd49dzTh5AoGAATf2\\ng0nN1QJehJEjPhqNYVl0HO3sMynIBaxNko0j8hxei1gFs5fx/X2o2Rp/Ke2C5H8X\\nfNziNpT7KB+Y1tuODAMhQPOR3XCWW6xZI0u9g6Li0w3CE1HqVyeEp/kptJJhfFwl\\n27zCoChhjI2mvxhdeQCDlIU2ptY5YsnjodiiNXECgYBluoVxuEnERjngK8uOzGD5\\nlzogapXXhAtXRRsLr1QLKF18RmsNgtkGgdeifMIK49siLbJnyZ6gv9GhxwDDitmz\\nRwyDVAfaiN5IgsHW/nLI7DvTXvCvOGZVAm9y1Ib/zCWvw27QdzYM+CV1t/JCnl0o\\n/o/LZ8PBzpBicWf6Oenm+A==\\n-----END PRIVATE KEY-----\\n\",\n" +
                    "  \"client_email\": \"firebase-adminsdk-fbsvc@marketplace-7143e.iam.gserviceaccount.com\",\n" +
                    "  \"client_id\": \"108314141242457751356\",\n" +
                    "  \"auth_uri\": \"https://accounts.google.com/o/oauth2/auth\",\n" +
                    "  \"token_uri\": \"https://oauth2.googleapis.com/token\",\n" +
                    "  \"auth_provider_x509_cert_url\": \"https://www.googleapis.com/oauth2/v1/certs\",\n" +
                    "  \"client_x509_cert_url\": \"https://www.googleapis.com/robot/v1/metadata/x509/firebase-adminsdk-fbsvc%40marketplace-7143e.iam.gserviceaccount.com\",\n" +
                    "  \"universe_domain\": \"googleapis.com\"\n" +
                    "}"
            val stream = ByteArrayInputStream(jasonString.toByteArray(StandardCharsets.UTF_8))

            val googleCredential = GoogleCredentials.fromStream(stream)
                .createScoped(arrayListOf(firebaseMessagingScope))

            googleCredential.refresh()

            return googleCredential.accessToken.tokenValue
        }catch (e:IOException){
            return null
        }
    }
}