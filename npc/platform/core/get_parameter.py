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
    with open("./build/report.txt", 'r') as file:
        lines = file.readlines()

    inst_cnt    = int(lines[0].strip()) 
    clk_cnt     = int(lines[1].strip())       
    ipc         = float(lines[2].strip())           
    ifu_pc      = float(lines[3].strip())           
    lsu_pc      = float(lines[4].strip())           
    alu_pc      = float(lines[5].strip())     
    i_LS        = float(lines[6].strip())
    i_CSR       = float(lines[7].strip())
    i_Cal       = float(lines[8].strip())

    return inst_cnt, clk_cnt, ipc, ifu_pc, lsu_pc, alu_pc, i_LS, i_CSR, i_Cal

def truncate_string(input_str, max_length):
    # 检查字符串是否超过最大长度
    if len(input_str) > max_length:
        # 截断并在末尾添加省略号
        return input_str[:max_length - 3] + "..."
    else:
        # 如果字符串未超过最大长度，则直接返回原字符串
        return input_str
    
def add_newlines(text, length):
    # 使用列表推导式，每隔指定长度分割一次，并在每段后添加换行符
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

    inst_cnt, clk_cnt, ipc, ifu_pc, lsu_pc, alu_pc, i_LS, i_CSR, i_Cal = read_report()

    print("Instruction count: ", inst_cnt)
    print("Clock count: ", clk_cnt)
    print("IPC: ", ipc)
    print("IFU performence counter: ", ifu_pc)
    print("LSU performence counter: ", lsu_pc)
    print("ALU performence counter: ", alu_pc)

    print("Freq: ", get_Freq())
    print("Chip area: ", get_Chip_area())
