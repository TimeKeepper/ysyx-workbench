import pandas as pd
from tabulate import tabulate
import re
import git

def get_latest_commit_id():
    repo = git.Repo(search_parent_directories=True)
    
    latest_commit = repo.head.commit
    
    return latest_commit.hexsha

def truncate_string(input_str, max_length):
    if len(input_str) > max_length:
        return input_str[:max_length - 3] + "..."
    else:
        return input_str

rpt = pd.read_csv("./build/result/ysyx_23060198.rpt", sep='|', skiprows=2, header=0)
Freq = rpt[' Freq(MHz) ']

stat_path = './build/result/synth_stat.txt'

with open(stat_path, 'r') as f:
    content = f.read()

stat = re.search(r"Chip area for top module '\\ysyx_23060198': ([\d\.]+)", content)

latest_commit_id = truncate_string(str(get_latest_commit_id()), 15)

df = {
    'Commit': [latest_commit_id],
    'Freq(MHz)': [Freq[1]],
    'Chip area(um^2)': [stat.group(1)]
}

print(tabulate(df, headers='keys', tablefmt='grid'))
