#!/bin/bash

# todo - move options to config file

certs_source=/opt/certs
passwords_source="$certs_source/passwords"

if [ -z ${EMAIL} ]
then
    echo "EMAIL is not set. Please set EMAIL"
    exit 1
fi

if [ ! -d $certs_source ]
then
    echo "Certitificates path does not exist. Missing: $certs_source"
    exit 1
fi

if [ ! -f $passwords_source ]
then
    echo "Passwords are not defined. Missing: $passwords_source"
    echo "Please define CA_PASS, KEY_PASS"
    exit 1
fi

source $passwords_source

environment=${ENVIRONMENT:-'development'}
echo "Running in $environment mode"

# create let's encrypt certs only if not deploying in dev mode

if [[ "$environment" != 'development' ]]
then

    if [ -z ${DOMAIN} ]
    then
        echo "DOMAIN is not set. Please set DOMAIN"
        exit 1
    fi

    echo "Creating public certitificates in /etc/letsencrypt/live/$DOMAIN..."

    # This currently is not working

    certbot certonly --agree-tos --email "$EMAIL" -d "$DOMAIN" --noninteractive
fi

domains=$DOMAIN

alt_names=""

ca_name=""

if [ "$environment" = 'development' ]
then
    localhost_name="localhost"

    echo "Adding development alt names..."
    read -r -d '' alt_names << EOM
DNS.2 = localhost
DNS.3 = proxy
EOM
    echo "$alt_names"

    ca_name="$localhost_name-root"

    if [ ! -d "$certs_source/$ca_name" ]
    then 
        mkdir $certs_source/$ca_name
    fi

    if [ ! -f "$certs_source/$ca_name/$ca_name.CA.pem" ]
    then
        echo "Creating $ca_name.CA.pem"

        pushd $certs_source/$ca_name> /dev/null

        openssl genrsa -out "$ca_name.CA.key" 2048
        openssl req -x509 -new -nodes \
            -key "$ca_name.CA.key" \
            -sha256 -days 365 -out "$ca_name.CA.pem" \
            -subj "/C=US/ST=CO/L=Golden/O=Colorado School of Mines/OU=Grading System/CN=Grading System Localhost Root CA"
        popd > /dev/null
    fi


fi

for domain in $domains
do
    if [ ! -d "$certs_source/$domain" ]
    then
        mkdir $certs_source/$domain
    fi

    pushd $certs_source/$domain > /dev/null

    if [ -d "out" ]
    then 
        echo "Certs already exist for $domain"
        popd > /dev/null
        continue; 
    fi

    echo "Creating cert for $domain"

    mkdir out

    openssl genrsa -out "./out/$domain.key" 2048 
    openssl req -new -key "./out/$domain.key" -out "$domain.csr" \
        -subj "/C=US/ST=CO/L=Golden/O=Colorado School of Mines/OU=Grading System/CN=$domain"

    cat > "$domain.ext" << EOF
authorityKeyIdentifier=keyid,issuer
basicConstraints=CA:FALSE
keyUsage = digitalSignature, nonRepudiation, keyEncipherment, dataEncipherment
subjectAltName = @alt_names
[alt_names]
DNS.1 = $domain
$alt_names
EOF


    openssl x509 -req -in "$domain.csr" -CA "../$ca_name/$ca_name.CA.pem" -CAkey "../$ca_name/$ca_name.CA.key" \
        -CAcreateserial -out "./out/$domain.crt" -days 365 -sha256 -extfile "$domain.ext"

    popd > /dev/null
done


