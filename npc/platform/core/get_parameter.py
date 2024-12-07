import pandas as pd
from tabulate import tabulate
import re
import git

def get_latest_commit_id():
    repo = git.Repo(search_parent_directories=True)
    
    latest_commit = repo.head.commit
    
    return latest_commit.hexsha

def get_latest_commit_message():
    repo = git.Repo(search_parent_directories=True)
    
    latest_commit = repo.head.commit
    
    return latest_commit.message.strip()

def read_report():
    data = {}

    with open("./build/report.txt", 'r') as file:
        for line in file:
            match = re.match(r'(\w+):\s+([\d\s]+)', line)
            if match:
                key = match.group(1)
                values = list(map(int, match.group(2).split()))
                data[key] = values

    return data

def truncate_string(input_str, max_length):
    if len(input_str) > max_length:
        return input_str[:max_length - 3] + "..."
    else:
        return input_str
    
def add_newlines(text, length):
    return '\n'.join([text[i:i+length] for i in range(0, len(text), length)])


def get_commit_id():
    return add_newlines(str(get_latest_commit_id()), 15)

def get_commit_message():
    return add_newlines(str(get_latest_commit_message()), 15)

def get_Freq():
    rpt = pd.read_csv("./build/result/ysyx_23060198.rpt", sep='|', skiprows=2, header=0)
    Freq = rpt[' Freq(MHz) ']
    return str(Freq[1]).strip()

def get_Chip_area():
    stat_path = './build/result/synth_stat.txt'

    with open(stat_path, 'r') as f:
        content = f.read()

    stat = re.search(r"Chip area for top module '\\ysyx_23060198': ([\d\.]+)", content)
    return stat.group(1)

if __name__ == '__main__':
    print("Commit: ", get_latest_commit_id())

    data = read_report()

    print("CSR inst: ", data['CSR'][0], "CSR clk", data['CSR'][1])
    print("LS inst: ", data['LS'][0], "LS clk", data['LS'][1])
    print("Cal inst: ", data['Cal'][0], "Cal clk", data['Cal'][1])
    print("GP inst: ", data['GP'][0], "GP clk", data['GP'][1])

    print("ALU pc", data['ALU'][0])
    print("LSU pc", data['LSU'][0])
    print("IFU pc", data['IFU'][0])

    print("Cache map hit: ", data['Inst'][0], "Cache hit", data['Inst'][1])

    print("Freq: ", get_Freq())
    print("Chip area: ", get_Chip_area())
