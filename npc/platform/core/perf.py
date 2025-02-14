from decimal import getcontext
from matplotlib import gridspec
import get_parameter as gp
from tabulate import tabulate
import matplotlib.pyplot as plt
from typing import TypeVar, Union, List

data = gp.read_report()
Freq = float(gp.get_Freq())

T = TypeVar('T', int, float, complex)

def safe_divide(numerator: Union[T, List[T]], denominator: Union[T, List[T]], precision: int = 100) -> Union[T, List[T], float]:
    getcontext().prec = precision 

    try:
        if isinstance(numerator, list) and isinstance(denominator, list):
            return [safe_divide(n, d) for n, d in zip(numerator, denominator)]
        elif isinstance(numerator, list):
            return [safe_divide(n, denominator) for n in numerator]
        elif isinstance(denominator, list):
            return [safe_divide(numerator, d) for d in denominator]
        else:
            return numerator / denominator
    except ZeroDivisionError:
        return 0  


def tabulate_show():
    df = {
        'Commit': [gp.get_commit_id()],
        'Message': [gp.get_commit_message()],
        'Performance Index': [safe_divide(data['GP'][1], data['GP'][0] * Freq)],
        'Chip area(um^2)': [gp.get_Chip_area()],
        'IPC': [safe_divide(data['GP'][1], data['GP'][0])],
        'Freq(MHz)': [Freq],
        'Icache hit rate': [safe_divide(data['Inst'][0], data['GP'][1])],
        'AMAT' : [data['AMAT'][0]],
        'Simulation clk_cnt': [data['GP'][0]],
    }

    colalign = ("center",) * len(df)

    print(tabulate(df, headers='keys', tablefmt='grid', colalign=colalign))

def ui():
    def func(pct, allvalues):
        absolute = int(pct/100.*sum(allvalues))
        return f"{pct:.1f}%\n({absolute})"

    clk_nums = [data['CSR'][0], data['LS'][0], data['Cal'][0]]
    inst_nums = [data['CSR'][1], data['LS'][1], data['Cal'][1]]
    a_cycle = safe_divide(inst_nums, clk_nums)
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
