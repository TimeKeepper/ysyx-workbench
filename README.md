进行submodule初始化
```
git submodule init
git submodule update
```

安装依赖
```
sudo apt-get install build-essential man gcc-doc gdb git libreadline-dev libsdl2-dev libsdl2-image-dev libsdl2-ttf-dev libelf-dev bison flex llvm llvm-14 llvm-14-dev
```

安装mill,注意必须安装至少0.11以上的版本
```
sudo apt-get install default-jre
curl -L https://github.com/com-lihaoyi/mill/releases/download/0.11.12/0.11.12 > mill && chmod +x mill
sudo mv ./mill /usr/local/bin/
mill --version
```

安装verilator
```
sudo apt-get install git help2man perl python3 make autoconf g++ flex bison ccache
sudo apt-get install libgoogle-perftools-dev numactl perl-doc
sudo apt-get install libfl2
sudo apt-get install libfl-dev
sudo apt-get install zlibc zlib1g zlib1g-dev
git clone https://github.com/verilator/verilator
unset VERILATOR_ROOT
cd verilator
git checkout v5.008
autoconf
./configure
make -j 'nproc' (如果报错，直接make)
sudo make install
verilator --version
```

定义环境变量
```
NEMU_HOME, NPC_HOME, AM_HOME, YSYXSOC_HOME, NVBOARD_HOME, YOSYS_HOME
```
初始化YSYXSOC
```
ysyxsoc目录下运行make dev-init make verilog
```
初始化NEMU
```
在nemu目录下运行make menuconfig
然后make
```
初始化NPC
```
在npc各个platform中执行menuconfig初始化配置，然后编译
```
安装交叉编译环境和rtt编译环境
```
sudo apt-get install g++-riscv64-linux-gnu binutils-riscv64-linux-gnu scons

在rtt的am目录下运行make init
```

如果你遇到了找不到gnu/stubs-ilp32.h文件的错误，需要手动在/usr/riscv64-linux-gnu/include/gnu/stubs.h中将该include注释掉\
安装yosys综合器
```
pip install pandas tabulate gitpython wcwidth
sudo apt-get install libunwind-dev libyaml-cpp-dev libgomp1 libtcl8.6

在https://github.com/YosysHQ/oss-cad-suite-build/releases中下载最新的releases，解压后在bashrc中source其中的enviroment

进入yosys-sta目录下执行make init
若需要综合查看PPA，在npc目录下运行make syn
```

[lecture note]: https://ysyx.oscc.cc/docs/
