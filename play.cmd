@ECHO OFF
python %1 --player_seed 4 --engine_seed 4 --turns 1000 --scenario --food random --map_file %2 %3 %4 -e -v --log_dir game_logs
D:
cd %5
cd ants/logs
rename.bat