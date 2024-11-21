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
sudo apt-get install openjdk-17-jdk
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
sudo apt-get install zlibc zlib1g zlib1g-dev #(ignore if gives error)
git clone https://github.com/verilator/verilator
unset VERILATOR_ROOT
cd verilator
git checkout v5.008
autoconf
./configure
make -j`nproc` #如果报错，直接make
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
初始化rocket-chip
```
cd npc/rocket-chip
git submodule update --init --recursive
```
初始化rvdecoderdb
```
cd npc/rvdecoderdb
git submodule update --init --recursive
```
安装espresso
```
进入url找到对应二进制文件https://github.com/chipsalliance/espresso
curl -JLO <url>
sudo mv ./espresso /usr/local/bin/
```
初始化NPC
```
在npc各个platform中执行menuconfig初始化配置，然后编译
```

安装交叉编译环境和rtt编译环境
```
sudo apt-get install g++-riscv64-linux-gnu binutils-riscv64-linux-gnu scons

在rtt的am目录下运行make init
make ARCH=riscv32e-ysyxsoc
```

如果你遇到了找不到gnu/stubs-ilp32.h文件的错误，需要手动在/usr/riscv64-linux-gnu/include/gnu/stubs.h中将该include注释掉\
安装yosys综合器
```
pip install pandas tabulate gitpython wcwidth
sudo apt-get install libunwind-dev libyaml-cpp-dev libgomp1 libtcl8.6 tcl-dev

git clone git@github.com:YosysHQ/yosys.git
cd yosys
make config-gcc
git submodule update --init --recursive
make -j`nproc`
sudo make install

进入yosys-sta目录下执行make init
若需要综合查看PPA，在npc目录下运行make syn
```

安装python依赖
```
pip install matplotlib
在npc下运行make perf查看效果
```

代码提示(metals)
```
安装coursier
curl -fL "https://github.com/coursier/launchers/raw/master/cs-x86_64-pc-linux.gz" | gzip -d > cs
chmod +x cs
sudo mv cs /usr/bin/
cs setup
cs install bloop
bloop

中途如果爆UNZIP什么错的话
rm -r ~/.cache/coursier
```

[lecture note]: https://ysyx.oscc.cc/docs/
