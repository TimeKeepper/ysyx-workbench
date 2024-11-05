import get_parameter as gp
from tabulate import tabulate
import matplotlib.pyplot as plt

inst_cnt, clk_cnt, ipc, ifu_pc, lsu_pc, alu_pc, i_LS, i_CSR, i_Cal, c_LS, c_CSR, c_Cal = gp.read_report()
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
    def func(pct, allvalues):
        absolute = int(pct/100.*sum(allvalues))
        return f"{pct:.1f}%\n({absolute})"

    inst_nums = [i_LS, i_CSR, i_Cal]
    clk_nums = [c_LS, c_CSR, c_Cal]
    a_cycle = [c_LS / i_LS, c_CSR / i_CSR, c_Cal / i_Cal]
    labels = ['LS', 'CSR', 'Cal']
    colors = ['#ff9999','#66b3ff','#99ff99']

    fig, axes = plt.subplots(2, 2, figsize=(10, 5))

    axes[0, 0].barh(labels, a_cycle, color=colors)
    axes[0, 0].set_xlabel('average ccycle')
    axes[0, 0].set_ylabel('instruction type')
    axes[0, 0].grid(axis='x', linestyle='--', alpha=0.7)
    for index, value in enumerate(a_cycle):
        axes[0, 0].text(value + 0.1, index, str(value), va='center')

    axes[1, 1].axis('off')

    axes[1, 0].pie(inst_nums, labels=labels, colors=colors, autopct=lambda pct: func(pct, inst_nums), startangle=90)
    axes[1, 0].set_title('Instruction Count')

    axes[1, 1].pie(clk_nums, labels=labels, colors=colors, autopct=lambda pct: func(pct, clk_nums), startangle=90)
    axes[1, 1].set_title('Clock Count')

    plt.tight_layout()
    plt.show()


if __name__ == '__main__':
    tabulate_show()
    ui()
