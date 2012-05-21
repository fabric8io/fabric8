function download {
  echo "Downloading: $1";
  ret=`curl --write-out %{http_code} --silent --output $2 $1`;
  if [ "${ret}" -ne 200 ]; then
    echo "Download failed with code: ${ret}";
    rm $2;
  fi;
}