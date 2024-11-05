from matplotlib import gridspec
import get_parameter as gp
from tabulate import tabulate
import matplotlib.pyplot as plt

inst_cnt, clk_cnt, ipc, ifu_pc, lsu_pc, alu_pc, i_LS, i_CSR, i_Cal, c_LS, c_CSR, c_Cal = gp.read_report()
ac_LS, ac_CSR, ac_Cal = c_LS / i_LS, c_CSR / i_CSR, c_Cal / i_Cal
Freq = float(gp.get_Freq())

def tabulate_show():
    df = {
        'Commit': [gp.get_commit_id()],
        'Message': [gp.get_commit_message()],
        'Performance Index': [ipc * Freq],
        'Chip area(um^2)': [gp.get_Chip_area()],
        'IPC': [ipc],
        'Freq(MHz)': [Freq],
        'Simulation inst_cnt': [inst_cnt],
        'Simulation clk_cnt': [clk_cnt],
        'LS aver clk': [ac_LS],
        'CSR aver clk': [ac_CSR],
        'Cal aver clk': [ac_Cal],
        'IF %': [ifu_pc / inst_cnt * 100],
        'LS %': [lsu_pc / inst_cnt * 100],
        'AL %': [alu_pc / inst_cnt * 100],
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
    a_cycle = [ac_LS, ac_CSR, ac_Cal]
    labels = ['LS', 'CSR', 'Cal']
    colors = ['#ff9999','#66b3ff','#99ff99']

    fig = plt.figure(figsize=(10, 8))
    gs = gridspec.GridSpec(2, 2, height_ratios=[1, 2])

    ax1 = fig.add_subplot(gs[0, :])
    ax1.barh(labels, a_cycle, color=colors)
    ax1.set_xlabel('average ccycle')
    ax1.set_ylabel('instruction type')
    ax1.grid(axis='x', linestyle='--', alpha=0.7)
    for index, value in enumerate(a_cycle):
        ax1.text(value + 0.1, index, str("{:.3f}".format(value)), va='center')

    ax2 = fig.add_subplot(gs[1, 0])
    ax2.pie(inst_nums, labels=labels, colors=colors, autopct=lambda pct: func(pct, inst_nums), startangle=90)
    ax2.set_title('Instruction Count')

    ax3 = fig.add_subplot(gs[1, 1])
    ax3.pie(clk_nums, labels=labels, colors=colors, autopct=lambda pct: func(pct, clk_nums), startangle=90)
    ax3.set_title('Clock Count')

    plt.tight_layout()
    plt.savefig('report.png')


if __name__ == '__main__':
    tabulate_show()
    ui()
