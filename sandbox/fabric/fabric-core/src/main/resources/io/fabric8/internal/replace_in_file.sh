function replace_in_file {
  sed "s/$1/$2/g" $3 > $3.tmp
  rm $3
  mv $3.tmp $3
}
