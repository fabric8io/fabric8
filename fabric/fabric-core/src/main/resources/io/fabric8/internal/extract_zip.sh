function extract_zip {
  if ! which unzip &> /dev/null; then
        jar xf $1
  else
       unzip $1
  fi
}