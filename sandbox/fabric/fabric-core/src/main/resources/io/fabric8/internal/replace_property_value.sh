function replace_property_value {
  echo "Setting value $2 for key $1 in $3"
  sed "s/$1.*=.*/$1 = $2/g" $3 > $3.tmp
  rm $3
  mv $3.tmp $3
}
