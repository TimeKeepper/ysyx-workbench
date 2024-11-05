import get_parameter as gp
from tabulate import tabulate

inst_cnt, clk_cnt, ipc, ifu_pc, lsu_pc, alu_pc = gp.read_report()
Freq = float(gp.get_Freq())

df = {
    'Commit': [gp.get_commit_id()],
    'Message': [gp.get_commit_message()],
    'Performance Index': [ipc * Freq],
    'Freq(MHz)': [Freq],
    'Chip area(um^2)': [gp.get_Chip_area()],
    'Simulation clk_cnt': [clk_cnt],
    'IPC': [ipc],
    'IFU PC': [ifu_pc],
    'LSU PC': [lsu_pc],
    'ALU PC': [alu_pc],
    'Simulation inst_cnt': [inst_cnt],
    'Total time(us)': [clk_cnt / Freq],
}

colalign = ("center",) * len(df)

if __name__ == '__main__':
    print(tabulate(df, headers='keys', tablefmt='grid', colalign=colalign))
