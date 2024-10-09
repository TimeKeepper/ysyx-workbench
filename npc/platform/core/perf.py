import get_parameter as gp
from tabulate import tabulate

inst_cnt, clk_cnt, ipc, ifu_pc, lsu_pc, alu_pc = gp.read_report()

df = {
    'Commit': [gp.get_commit_id()],
    'Message': [gp.get_commit_message()],
    'Simulation clk_cnt': [clk_cnt],
    'Simulation inst_cnt': [inst_cnt],
    'IPC': [ipc],
    'IFU PC': [ifu_pc],
    'LSU PC': [lsu_pc],
    'ALU PC': [alu_pc],
    'Freq(MHz)': [gp.get_Freq()],
    'Chip area(um^2)': [gp.get_Chip_area()]
}

colalign = ("center",) * len(df)

if __name__ == '__main__':
    print(tabulate(df, headers='keys', tablefmt='grid', colalign=colalign))
