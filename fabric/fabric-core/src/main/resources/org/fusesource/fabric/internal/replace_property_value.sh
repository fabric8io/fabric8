function replace_property_value {
  sed "s/$1.*=.*/$1 = $2/g" $3 > $3.tmp
  rm $3
  mv $3.tmp $3
}
