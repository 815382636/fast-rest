#!/bin/bash
for a in 99994 99996 99998
do
        for b in 2.1 2.3 2.5 2.7 2.9
        do
                python autorun.py $1 0 $a $b
        done
done
