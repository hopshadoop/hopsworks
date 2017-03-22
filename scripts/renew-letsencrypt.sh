#!/bin/bash


if [ $# -lt 5 ] ; then

    echo "usage: <prog> DOMAIN keystore_passwd glassfish_user glassfish_domain_dir renewal_num [lets_encrypt_base_dir]"
    echo "e.g."
    echo "./$0 hops.site adminpw glassfish /srv/glassfish/domain1 0001"
    echo "./$0 hops.site adminpw glassfish /srv/glassfish/domain1 0003 /etc/letsencyrpt/live"    
    exit 1
fi

DOMAIN=$1
DOMAIN=hops.site
NEW_DOMAIN=${DOMAIN}-$5
KEYSTOREPW=$2
GF_USER=$3
GF_DOMAIN=$4
#KEYSTOREPW=adminpw
#GF_USER=glassfish
#GF_DOMAIN=/srv/glassfish/domain1

BASE=/etc/letsencrypt/live 
if [ $# -gt 5 ] ; then
    BASE=$6
fi    

LIVE=${BASE}/$DOMAIN
RENEWED=${BASE}/$NEW_DOMAIN


#Get new EFF-Certbot

set -e

if [ $# -ne 1 ] ; then
  echo "usage: $0 number"
  echo "e.g., $0 0001"
  echo "e.g., $0 0002"
  echo "..."
  echo "e.g., $0 0010"
  exit 1
fi

sudo service glassfish-domain1 stop


#Create Script, Make it executable



#git clone https://github.com/certbot/certbot
#cd certbot
#git pul
sudo rm -rf /etc/letsencrypt/live/${DOMAIN}.old
sudo mv  ${LIVE} /etc/letsencrypt/live/${DOMAIN}.old
cd / 
sudo /home/hdp/opt/certbot/certbot-auto certonly --standalone -d $DOMAIN -d www.$DOMAIN


mv -f $RENEWED $LIVE


#Create new Keystore using the LetsEncrypt Certificates
sudo openssl pkcs12 -export -in $LIVE/cert.pem -inkey $LIVE/privkey.pem -out cert_and_key.p12 -name $DOMAIN -CAfile $LIVE/chain.pem -caname root -password pass:$KEYSTOREPW
sudo keytool -importkeystore -destkeystore keystore.jks -srckeystore cert_and_key.p12 -srcstoretype PKCS12 -alias $DOMAIN -srcstorepass $KEYSTOREPW -deststorepass $KEYSTOREPW -destkeypass $KEYSTOREPW
sudo keytool -import -noprompt -trustcacerts -alias root -file $LIVE/chain.pem -keystore keystore.jks -srcstorepass $KEYSTOREPW -deststorepass $KEYSTOREPW -destkeypass $KEYSTOREPW

sudo openssl pkcs12 -export -in $LIVE/fullchain.pem -inkey $LIVE/privkey.pem -out pkcs.p12 -name glassfish-instance -password pass:$KEYSTOREPW
sudo keytool -importkeystore -destkeystore keystore.jks -srckeystore pkcs.p12 -srcstoretype PKCS12 -alias glassfish-instance -srcstorepass $KEYSTOREPW -deststorepass $KEYSTOREPW -destkeypass $KEYSTOREPW
sudo openssl pkcs12 -export -in $LIVE/fullchain.pem -inkey $LIVE/privkey.pem -out pkcs.p12 -name s1as -password pass:$KEYSTOREPW
sudo keytool -importkeystore -destkeystore keystore.jks -srckeystore pkcs.p12 -srcstoretype PKCS12 -alias s1as -srcstorepass $KEYSTOREPW -deststorepass $KEYSTOREPW -destkeypass $KEYSTOREPW

#Add new Certificates to Truststore
sudo keytool -export -alias glassfish-instance -file glassfish-instance.cert -keystore keystore.jks -storepass $KEYSTOREPW
sudo keytool -export -alias s1as -file s1as.cert -keystore keystore.jks -storepass $KEYSTOREPW

sudo keytool -import -noprompt -alias s1as -file s1as.cert -keystore cacerts.jks -storepass $KEYSTOREPW
sudo keytool -import -noprompt -alias glassfish-instance -file glassfish-instance.cert -keystore cacerts.jks -storepass $KEYSTOREPW

rm -f cert_and_key.p12 glassfish-instance.cert pkcs.p12 s1as.cert

#3. Copy keystore.jks & cacerts.jks to domain1/config dir of glassfish

mv -f $GF_DOMAIN/config/keystore.jks $GF_DOMAIN/config/keystore.jks.old
cp -f keystore.jks $GF_DOMAIN/config
mv -f $GF_DOMAIN/config/cacerts.jks $GF_DOMAIN/config/cacerts.jks.old
cp -f cacerts.jks $GF_DOMAIN/config
chown $GF_USER $GF_DOMAIN/config/*.jks
chmod 600 $GF_DOMAIN/config/*.jks


sudo service glassfish-domain1 start

cd $GF_DOMAIN/bin

./domain1_asadmin enable-secure-admin
./domain1_asadmin set server.network-config.protocols.protocol.http-listener-2.ssl.ssl3-enabled=false
./domain1_asadmin set server.network-config.protocols.protocol.sec-admin-listener.ssl.ssl3-enabled=false
./domain1_asadmin set server.iiop-service.iiop-listener.SSL.ssl.ssl3-enabled=false
./domain1_asadmin set server.iiop-service.iiop-listener.SSL_MUTUALAUTH.ssl.ssl3-enabled=false


./domain1_asadmin set 'configs.config.server-config.network-config.protocols.protocol.http-listener-2.ssl.ssl3-tls-ciphers=+TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,+TLS_RSA_WITH_AES_128_CBC_SHA256,+TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256,+TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,-TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,-TLS_DHE_DSS_WITH_AES_128_CBC_SHA256,+TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,+TLS_RSA_WITH_AES_128_CBC_SHA,+TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,+TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,-TLS_DHE_RSA_WITH_AES_128_CBC_SHA,-TLS_DHE_DSS_WITH_AES_128_CBC_SHA,+TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_RSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,-TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,-TLS_ECDHE_RSA_WITH_RC4_128_SHA,-SSL_RSA_WITH_RC4_128_SHA,-TLS_ECDH_ECDSA_WITH_RC4_128_SHA,-TLS_ECDH_RSA_WITH_RC4_128_SHA,-SSL_RSA_WITH_RC4_128_MD5,-TLS_EMPTY_RENEGOTIATION_INFO_SCSV,-TLS_DH_anon_WITH_AES_128_CBC_SHA256,-TLS_ECDH_anon_WITH_AES_128_CBC_SHA,-TLS_DH_anon_WITH_AES_128_CBC_SHA,-TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA,-SSL_DH_anon_WITH_3DES_EDE_CBC_SHA,-TLS_ECDH_anon_WITH_RC4_128_SHA,-SSL_DH_anon_WITH_RC4_128_MD5,-SSL_RSA_WITH_DES_CBC_SHA,-SSL_DHE_RSA_WITH_DES_CBC_SHA,-SSL_DHE_DSS_WITH_DES_CBC_SHA,-SSL_DH_anon_WITH_DES_CBC_SHA,-SSL_RSA_EXPORT_WITH_RC4_40_MD5,-SSL_DH_anon_EXPORT_WITH_RC4_40_MD5,-SSL_RSA_EXPORT_WITH_DES40_CBC_SHA,-SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,-SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA,-SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA,-TLS_RSA_WITH_NULL_SHA256,-TLS_ECDHE_ECDSA_WITH_NULL_SHA,-TLS_ECDHE_RSA_WITH_NULL_SHA,-SSL_RSA_WITH_NULL_SHA,-TLS_ECDH_ECDSA_WITH_NULL_SHA,-TLS_ECDH_RSA_WITH_NULL_SHA,-TLS_ECDH_anon_WITH_NULL_SHA,-SSL_RSA_WITH_NULL_MD5'


./domain1_asadmin set 'configs.config.server-config.network-config.protocols.protocol.sec-admin-listener.ssl.ssl3-tls-ciphers=+TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,+TLS_RSA_WITH_AES_128_CBC_SHA256,+TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256,+TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,-TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,-TLS_DHE_DSS_WITH_AES_128_CBC_SHA256,+TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,+TLS_RSA_WITH_AES_128_CBC_SHA,+TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,+TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,-TLS_DHE_RSA_WITH_AES_128_CBC_SHA,-TLS_DHE_DSS_WITH_AES_128_CBC_SHA,+TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_RSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,-TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,-TLS_ECDHE_RSA_WITH_RC4_128_SHA,-SSL_RSA_WITH_RC4_128_SHA,-TLS_ECDH_ECDSA_WITH_RC4_128_SHA,-TLS_ECDH_RSA_WITH_RC4_128_SHA,-SSL_RSA_WITH_RC4_128_MD5,-TLS_EMPTY_RENEGOTIATION_INFO_SCSV,-TLS_DH_anon_WITH_AES_128_CBC_SHA256,-TLS_ECDH_anon_WITH_AES_128_CBC_SHA,-TLS_DH_anon_WITH_AES_128_CBC_SHA,-TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA,-SSL_DH_anon_WITH_3DES_EDE_CBC_SHA,-TLS_ECDH_anon_WITH_RC4_128_SHA,-SSL_DH_anon_WITH_RC4_128_MD5,-SSL_RSA_WITH_DES_CBC_SHA,-SSL_DHE_RSA_WITH_DES_CBC_SHA,-SSL_DHE_DSS_WITH_DES_CBC_SHA,-SSL_DH_anon_WITH_DES_CBC_SHA,-SSL_RSA_EXPORT_WITH_RC4_40_MD5,-SSL_DH_anon_EXPORT_WITH_RC4_40_MD5,-SSL_RSA_EXPORT_WITH_DES40_CBC_SHA,-SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,-SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA,-SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA,-TLS_RSA_WITH_NULL_SHA256,-TLS_ECDHE_ECDSA_WITH_NULL_SHA,-TLS_ECDHE_RSA_WITH_NULL_SHA,-SSL_RSA_WITH_NULL_SHA,-TLS_ECDH_ECDSA_WITH_NULL_SHA,-TLS_ECDH_RSA_WITH_NULL_SHA,-TLS_ECDH_anon_WITH_NULL_SHA,-SSL_RSA_WITH_NULL_MD5'


./domain1_asadmin set 'configs.config.server-config.iiop-service.iiop-listener.SSL.ssl.ssl3-tls-ciphers=+TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,+TLS_RSA_WITH_AES_128_CBC_SHA256,+TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256,+TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,-TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,-TLS_DHE_DSS_WITH_AES_128_CBC_SHA256,+TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,+TLS_RSA_WITH_AES_128_CBC_SHA,+TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,+TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,-TLS_DHE_RSA_WITH_AES_128_CBC_SHA,-TLS_DHE_DSS_WITH_AES_128_CBC_SHA,+TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_RSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,-TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,-TLS_ECDHE_RSA_WITH_RC4_128_SHA,-SSL_RSA_WITH_RC4_128_SHA,-TLS_ECDH_ECDSA_WITH_RC4_128_SHA,-TLS_ECDH_RSA_WITH_RC4_128_SHA,-SSL_RSA_WITH_RC4_128_MD5,-TLS_EMPTY_RENEGOTIATION_INFO_SCSV,-TLS_DH_anon_WITH_AES_128_CBC_SHA256,-TLS_ECDH_anon_WITH_AES_128_CBC_SHA,-TLS_DH_anon_WITH_AES_128_CBC_SHA,-TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA,-SSL_DH_anon_WITH_3DES_EDE_CBC_SHA,-TLS_ECDH_anon_WITH_RC4_128_SHA,-SSL_DH_anon_WITH_RC4_128_MD5,-SSL_RSA_WITH_DES_CBC_SHA,-SSL_DHE_RSA_WITH_DES_CBC_SHA,-SSL_DHE_DSS_WITH_DES_CBC_SHA,-SSL_DH_anon_WITH_DES_CBC_SHA,-SSL_RSA_EXPORT_WITH_RC4_40_MD5,-SSL_DH_anon_EXPORT_WITH_RC4_40_MD5,-SSL_RSA_EXPORT_WITH_DES40_CBC_SHA,-SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,-SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA,-SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA,-TLS_RSA_WITH_NULL_SHA256,-TLS_ECDHE_ECDSA_WITH_NULL_SHA,-TLS_ECDHE_RSA_WITH_NULL_SHA,-SSL_RSA_WITH_NULL_SHA,-TLS_ECDH_ECDSA_WITH_NULL_SHA,-TLS_ECDH_RSA_WITH_NULL_SHA,-TLS_ECDH_anon_WITH_NULL_SHA,-SSL_RSA_WITH_NULL_MD5'


./domain1_asadmin set 'configs.config.server-config.iiop-service.iiop-listener.SSL_MUTUALAUTH.ssl.ssl3-tls-ciphers=+TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256,+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256,+TLS_RSA_WITH_AES_128_CBC_SHA256,+TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256,+TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256,-TLS_DHE_RSA_WITH_AES_128_CBC_SHA256,-TLS_DHE_DSS_WITH_AES_128_CBC_SHA256,+TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA,+TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA,+TLS_RSA_WITH_AES_128_CBC_SHA,+TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA,+TLS_ECDH_RSA_WITH_AES_128_CBC_SHA,-TLS_DHE_RSA_WITH_AES_128_CBC_SHA,-TLS_DHE_DSS_WITH_AES_128_CBC_SHA,+TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_RSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA,+TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA,-SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA,-TLS_ECDHE_ECDSA_WITH_RC4_128_SHA,-TLS_ECDHE_RSA_WITH_RC4_128_SHA,-SSL_RSA_WITH_RC4_128_SHA,-TLS_ECDH_ECDSA_WITH_RC4_128_SHA,-TLS_ECDH_RSA_WITH_RC4_128_SHA,-SSL_RSA_WITH_RC4_128_MD5,-TLS_EMPTY_RENEGOTIATION_INFO_SCSV,-TLS_DH_anon_WITH_AES_128_CBC_SHA256,-TLS_ECDH_anon_WITH_AES_128_CBC_SHA,-TLS_DH_anon_WITH_AES_128_CBC_SHA,-TLS_ECDH_anon_WITH_3DES_EDE_CBC_SHA,-SSL_DH_anon_WITH_3DES_EDE_CBC_SHA,-TLS_ECDH_anon_WITH_RC4_128_SHA,-SSL_DH_anon_WITH_RC4_128_MD5,-SSL_RSA_WITH_DES_CBC_SHA,-SSL_DHE_RSA_WITH_DES_CBC_SHA,-SSL_DHE_DSS_WITH_DES_CBC_SHA,-SSL_DH_anon_WITH_DES_CBC_SHA,-SSL_RSA_EXPORT_WITH_RC4_40_MD5,-SSL_DH_anon_EXPORT_WITH_RC4_40_MD5,-SSL_RSA_EXPORT_WITH_DES40_CBC_SHA,-SSL_DHE_RSA_EXPORT_WITH_DES40_CBC_SHA,-SSL_DHE_DSS_EXPORT_WITH_DES40_CBC_SHA,-SSL_DH_anon_EXPORT_WITH_DES40_CBC_SHA,-TLS_RSA_WITH_NULL_SHA256,-TLS_ECDHE_ECDSA_WITH_NULL_SHA,-TLS_ECDHE_RSA_WITH_NULL_SHA,-SSL_RSA_WITH_NULL_SHA,-TLS_ECDH_ECDSA_WITH_NULL_SHA,-TLS_ECDH_RSA_WITH_NULL_SHA,-TLS_ECDH_anon_WITH_NULL_SHA,-SSL_RSA_WITH_NULL_MD5'

