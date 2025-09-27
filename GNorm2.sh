#!/bin/sh

#
# ./GNorm2.sh input output
#
INPUT=$(realpath ${1})
OUTPUT=$(realpath ${2})
BASE_DIR=$(realpath ${3})
SETUP_DIR=$(realpath ${4})
NERMODEL=$(realpath ${5})
SAMODEL=$(realpath ${6})
RESOURCES_DIR=$(realpath ${7})
cd ${BASE_DIR}

# 创建必要的目录
mkdir -p ${OUTPUT}/tmp_SR
mkdir -p ${OUTPUT}/tmp_GNR  
mkdir -p ${OUTPUT}/tmp_SA
mkdir -p tmp  # Java代码中使用的tmp目录

# SR
java -Xmx60G -Xms30G -jar GNormPlus.jar ${INPUT} ${OUTPUT}/tmp_SR ${SETUP_DIR}/setup.SR.txt

# GNR+SA (CUDA_VISIBLE_DEVICES=3 )
python GeneNER_SpeAss_run.py -i ${OUTPUT}/tmp_SR -r ${OUTPUT}/tmp_GNR -a ${OUTPUT}/tmp_SA -n ${NERMODEL} -s ${SAMODEL} -b ${RESOURCES_DIR}

# GN
java -Xmx60G -Xms30G -jar GNormPlus.jar ${OUTPUT}/tmp_SA ${OUTPUT} ${SETUP_DIR}/setup.GN.txt

# rm tmp_SR/*
# rm tmp_GNR/*
# rm tmp_SA/*
