import get_parameter as gp
from tabulate import tabulate
import matplotlib.pyplot as plt

inst_cnt, clk_cnt, ipc, ifu_pc, lsu_pc, alu_pc, i_LS, i_CSR, i_Cal = gp.read_report()
Freq = float(gp.get_Freq())

def tabulate_show():
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

    print(tabulate(df, headers='keys', tablefmt='grid', colalign=colalign))

def ui():
    sizes = [i_LS, i_CSR, i_Cal]
    labels = ['LS', 'CSR', 'Cal']
    colors = ['#ff9999','#66b3ff','#99ff99']

    plt.pie(sizes, labels=labels, colors=colors, autopct='%1.1f%%', startangle=90)
    plt.axis('equal')
    plt.show()

if __name__ == '__main__':
    tabulate_show()
    ui()
