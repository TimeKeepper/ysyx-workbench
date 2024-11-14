#include <utility>
#include <utils.hpp>
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <cassert>

template <typename T>
T& getCircularElement(std::vector<T>& vec, int index) {
    int size = vec.size();
    // 确保索引总是落在 [0, size - 1] 的范围内
    int circularIndex = (index % size + size) % size; // 处理负索引的情况
    return vec[circularIndex];
}

std::vector<std::uint16_t> bram_ui;
std::vector<std::uint16_t> bram_pointer;

const std::string file_path = "/home/wenjiu/ysyx-workbench/npc/platform/BASYS/src/vga_coe/";

std::vector<std::pair<std::string, std::vector<std::uint16_t>&>> bram_list = {
    {"ui", bram_ui},
    {"pointer", bram_pointer},
};

void bram_init(){

    for(auto& [name, bram] : bram_list) {
        std::ifstream file(file_path + name + ".coe");
        if(!file.is_open()){
            Log("File not found: %s", name.c_str());
            continue;
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

        Log("BRAM %s size: %lu", name.c_str(), bram.size());

        file.close();
    }
}

extern "C" void bram_ui_read(int raddr, int *rdata){
    // static int last_raddr = -1;

    // if(raddr != last_raddr) {
    //     if(raddr != last_raddr + 1) {
    //         Log("BRAM read not continuous: %d -> %d", last_raddr, raddr);
    //     }
    // }
    // last_raddr = raddr;

    *rdata = getCircularElement(bram_ui, raddr);
}

extern "C" void bram_pointer_read(int raddr, int *rdata){
    *rdata = getCircularElement(bram_pointer, raddr);
}

#include <SDL2/SDL.h>
#include <thread>
#include <chrono>
#include <algorithm>
int x, y, oth;

bool thread_run = true;

void mouse_catch(){
    if (SDL_Init(SDL_INIT_VIDEO | SDL_INIT_EVENTS) < 0) {
        std::cerr << "SDL initialization failed: " << SDL_GetError() << std::endl;
        return;
    }

    while(thread_run) {
        SDL_GetGlobalMouseState(&x, &y);
        std::this_thread::sleep_for(std::chrono::milliseconds(1));
        // std::cout << "x: " << x << " y: " << y << std::endl;
    }

    SDL_Quit();
}

extern "C" void mouse_sim(int *data){
    static int last_x = x, last_y = y;

    int32_t bias_x = (x - last_x);
    int32_t bias_y = (last_y - y);

    *data = ((bias_x << 8) & 0x0000ff00) | ((bias_y << 16) & 0x00ff0000);

    last_x = x;
    last_y = y;
}
