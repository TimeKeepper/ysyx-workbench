import get_parameter as gp
from tabulate import tabulate

df = {
    'Commit': [gp.get_commit_id()],
    'Message': [gp.get_commit_message()],
    'Freq(MHz)': [gp.get_Freq()],
    'Chip area(um^2)': [gp.get_Chip_area()]
}

if __name__ == '__main__':
    print(tabulate(df, headers='keys', tablefmt='grid'))
