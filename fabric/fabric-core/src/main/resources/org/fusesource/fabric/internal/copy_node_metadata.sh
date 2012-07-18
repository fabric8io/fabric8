function copy_node_metadata() {
  echo "Copying metadata for container: $1";
  TARGET_PATH="./fabric/import/fabric/registry/containers/config/$1/"
  mkdir -p $TARGET_PATH
  ENCODED_METADATA=$2
  echo $ENCODED_METADATA > ./fabric/import/fabric/registry/containers/config/$1/metadata.cfg
}