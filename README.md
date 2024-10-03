# "一生一芯"工程项目

这是"一生一芯"的工程项目. 通过运行
```bash
bash init.sh subproject-name
```
进行初始化, 具体请参考[实验讲义][lecture note].

安装依赖
```
apt-get install build-essential man gcc-doc gdb git libreadline-dev libsdl2-dev libsdl2-image-dev libsdl2-ttf-dev libelf-dev bison flex llvm llvm-14 llvm-14-dev
```

安装交叉编译环境和rtt编译环境
```
apt-get install g++-riscv64-linux-gnu binutils-riscv64-linux-gnu scons

拉取submodule
git submodule update --init --recursive

在rtt的am目录下运行make init
```

安装mill
```
sudo apt-get install default-jre
sed -i '0,/-cp "\$0"/{s/-cp "\$0"/-cp `cygpath -w "\$0"`/}; 0,/-cp "\$0"/{s/-cp "\$0"/-cp `cygpath -w "\$0"`/}' /usr/local/bin/mill
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
make -j 'nproc'
sudo make install
verilator --version
```

需要定义环境变量：NPC_HOME, AM_HOME, NEMU_HOME, YSYXSOC_HOME
需要在ysyxsoc目录下运行make dev-init make verilog

[lecture note]: https://ysyx.oscc.cc/docs/
