#include <utils.hpp>
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <cassert>

template <typename T>
T& cyclicAccess(std::vector<T>& vec, size_t index) {
    return vec[index % vec.size()];  // 自动回环
}

std::vector<std::uint16_t> bram;

void bram_init(){
    std::ifstream file("/home/wenjiu/ysyx-workbench/npc/platform/BASYS/src/vga_coe/ui.coe");
    if(!file.is_open()){
        Log("File not found");
        return;
    }

    std::string line;

    std::getline(file, line);
    std::getline(file, line);

    while (std::getline(file, line, ',')) {  // 用逗号分隔
        // 移除结尾的换行符和空格
        line.erase(line.find_last_not_of(" \n\r\t") + 1);

        if (!line.empty()) {
            if(line.back() == ';') break;
            uint16_t value;
            std::stringstream ss;
            ss << std::hex << line;  // 指定以16进制解析
            ss >> value;
            bram.push_back(value);
        }
    }

    Log("BRAM size: %lu", bram.size());

    file.close();
}

extern "C" void bram_ui_read(int raddr, int *rdata){
    // static int last_raddr = -1;

    // if(raddr != last_raddr) {
    //     if(raddr != last_raddr + 1) {
    //         Log("BRAM read not continuous: %d -> %d", last_raddr, raddr);
    //     }
    // }
    // last_raddr = raddr;

    *rdata = cyclicAccess(bram, raddr);
}
