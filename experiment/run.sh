unset -v expIteration
unset -v platform
unset -v viewtype
unset -v dataset

soc=false
word=false
oag=false
prov=false
lsqb=false

while getopts ":i:p:v:d:" opt
   do
     case $opt in
	i ) expIteration=("$OPTARG")
	    ;;
        p ) platform=("$OPTARG")
            ;;
	v ) viewtype=("$OPTARG")
	    ;;
        d ) dataset=("$OPTARG")
            ;;
     esac
done

# (Start) Settings
if [ -z "$platform" ] || [ -z "$dataset" ] || [ -z "$viewtype" ] || [ -z "expIteration" ]; then
    printf "Usage: ${0} -i [EXPITERATION] -p [PLATFORM] -v [VIEW] -d [DATASET]\n\n"
    printf "Examples:\n\t${0} -i 7 -p lb -v mv -d word\n"
    exit
fi

#echo expIteration : $expIteration
#echo platform : $platform
#echo viewtype: $viewtype
#echo dataset : $dataset

eval "$dataset=true"


#echo soc : $soc
#echo word : $word
#echo oag : $oag
#echo prov : $prov
#echo lsqb : $lsqb


sed "s|\${expIteration}|${expIteration}|g" workload/workload.json.template \
| sed "s|\${platform}|\"$platform\"|g" \
| sed "s|\${viewtype}|\"$viewtype\"|g" \
| sed "s|\${soc}|$soc|g" \
| sed "s|\${prov}|$prov|g" \
| sed "s|\${oag}|$oag|g" \
| sed "s|\${word}|$word|g" \
| sed "s|\${lsqb}|$lsqb|g" > workload/workload.json

cd ..
mvn exec:java@exp
cd -

