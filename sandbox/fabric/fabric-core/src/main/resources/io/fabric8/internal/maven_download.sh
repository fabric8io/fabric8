function maven_download {
  echo "Downloading Maven Artifact with groupId: $2 artifactId: $3 and version: $4 from repository: $1";
  export REPO=$1
  export GROUP_ID=$2
  export ARTIFACT_ID=$3
  export VERSION=$4
  export TYPE=$5
  export TARGET_FILE=$ARTIFACT_ID-$VERSION.$TYPE

  export GROUP_ID_PATH=`echo $GROUP_ID | sed 's/\./\//g'`

  export ARTIFACT_BASE_URL=`echo $REPO$GROUP_ID_PATH/$ARTIFACT_ID/$VERSION/`

  if [[ "$VERSION" == *SNAPSHOT* ]];  then
    export ARTIFACT_URL=`curl --silent $ARTIFACT_BASE_URL | grep href | grep zip\" | sed 's/^.*<a href="//' | sed 's/".*$//'  | tail -1`
  else
    export ARTIFACT_URL=`echo $REPO$GROUP_ID_PATH/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.$TYPE`
  fi

  if [ -z "$ARTIFACT_URL" ]; then
      export ARTIFACT_URL=`echo $REPO$GROUP_ID_PATH/$ARTIFACT_ID/$VERSION/$ARTIFACT_ID-$VERSION.$TYPE`
  fi

  echo "Using URL: $ARTIFACT_URL"
  ret=`curl --write-out %{http_code} --silent --output $TARGET_FILE $ARTIFACT_URL`
  if [ "${ret}" -ne 200 ]; then
    echo "Download failed with code: ${ret}"
    rm $TARGET_FILE
  fi
}
