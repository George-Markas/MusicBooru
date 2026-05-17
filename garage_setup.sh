#!/usr/bin/env bash

set -e

garage_cmd() {
    docker exec garage /garage "$@"
}

usage() {
    echo "Usage: $0 [-c <node capacity>] [-o <output directory>]"
    exit 2
}

NODE_CAPACITY="10G"
OUTPUT_DIR="$HOME"

while getopts "c:o:" opt
do
    case "$opt" in
    c) NODE_CAPACITY="$OPTARG" ;;
    o) OUTPUT_DIR="${OPTARG%/}" ;;
    ?) usage ;;
    esac
done

NODE_ID=$(garage_cmd node id --quiet)
NODE_ID="${NODE_ID%@*}"
KEY_NAME="musicbooru_key"

garage_cmd layout assign -z zone1 -c "$NODE_CAPACITY" "$NODE_ID" > /dev/null 2>&1
garage_cmd layout apply --version 1 > /dev/null 2>&1
echo "Applied layout"

garage_cmd bucket create library > /dev/null 2>&1
garage_cmd bucket create artwork > /dev/null 2>&1
echo "Created buckets"

garage_cmd key create "$KEY_NAME" > /dev/null 2>&1
garage_cmd bucket allow --read --write library --key "$KEY_NAME" > /dev/null 2>&1
garage_cmd bucket allow --read --write artwork --key "$KEY_NAME" > /dev/null 2>&1
echo "Granted access key to buckets"

KEY_INFO=$(garage_cmd key info --show-secret "$KEY_NAME" 2>/dev/null) 
KEY_ID=$(grep "Key ID:" <<< "$KEY_INFO" | awk '{print $3}')
SECRET_KEY=$(grep "Secret key:" <<< "$KEY_INFO" | awk '{print $3}')

cat > "$OUTPUT_DIR"/.awsrc <<EOF
export AWS_ENDPOINT_URL='http://localhost:3900'
export AWS_DEFAULT_REGION='musicbooru'
export AWS_ACCESS_KEY_ID='$KEY_ID'
export AWS_SECRET_ACCESS_KEY='$SECRET_KEY'
EOF
echo -e "\e[32mCreated .awsrc in $OUTPUT_DIR\e[0m"
