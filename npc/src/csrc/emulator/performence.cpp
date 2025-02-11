#include "common.hpp"
#include <performence.hpp>
#include <fstream>
#include <string>

// Function to convert Inst_Type to string
std::string inst_type_to_string(performence::Inst_Type type) {
    switch(type) {
        case performence::Inst_Type::GP: return "GP";
        case performence::Inst_Type::Cal: return "Cal";
        case performence::Inst_Type::LS: return "LS";
        case performence::Inst_Type::CSR: return "CSR";
        default: return "Unknown";
    }
}

performence::performence(){

    this->inst_cntrs.emplace(Inst_Type::GP, std::make_pair(0, 0));
    this->inst_cntrs.emplace(Inst_Type::Cal, std::make_pair(0, 0));
    this->inst_cntrs.emplace(Inst_Type::LS, std::make_pair(0, 0));
    this->inst_cntrs.emplace(Inst_Type::CSR, std::make_pair(0, 0));

    this->conpo_cntrs.emplace("IFU", 0);
    this->conpo_cntrs.emplace("LSU", 0);
    this->conpo_cntrs.emplace("ALU", 0);

    this->cache_cntrs.emplace("Inst", std::make_pair(0, 0));
}

performence::~performence(){
    std::string report_dir = "/home/wenjiu/ysyx-workbench/npc/platform/core/build/report.txt";

    std::ofstream report(report_dir, std::ios::out);

    for(auto &i : this->inst_cntrs){
        report << inst_type_to_string(i.first) << ":\t" << i.second.first << " " << i.second.second << std::endl;
    }

    for(auto &i : this->conpo_cntrs){
        report << i.first << ":\t" << i.second << std::endl;
    }

    for(auto &i : this->cache_cntrs){
        report << i.first << ":\t" << i.second.first << " " << i.second.second << std::endl;
    }

    this->inst_cntrs.clear();
    this->conpo_cntrs.clear();
    this->cache_cntrs.clear();
}

void performence::clk_count(){
    this->inst_cntrs[Inst_Type::GP].first += 1;
    if(unlikely(this->cur_instType == Inst_Type::GP)) return;

    this->inst_cntrs[this->cur_instType].first += 1;
}

void performence::inst_cont(){
    this->inst_cntrs[Inst_Type::GP].second += 1;
    if(unlikely(this->cur_instType == Inst_Type::GP)) return;
    
    this->inst_cntrs[this->cur_instType].second += 1;
}

void performence::inst_type_set(Inst_Type type){
    this->cur_instType = type;
}

void performence::coponent_count(const std::string& name){
    if(this->conpo_cntrs.find(name) == this->conpo_cntrs.end()){ 
        Log("Unknown component name: %s", name.c_str());
        return;
    }
    this->conpo_cntrs[name] += 1;
}

void performence::cache_count(const std::string& name, bool map_hit, bool cache_hit){
    if(this->cache_cntrs.find(name) == this->cache_cntrs.end()){ 
        Log("Unknown cache name: %s", name.c_str());
        return;
    }
    this->cache_cntrs[name].first += map_hit;
    this->cache_cntrs[name].second += cache_hit;
}

void performence::print_perf() {
    for(auto &i : this->inst_cntrs){
        // Log("%s:\t%ld %ld", inst_type_to_string(i.first).c_str(), i.second.first, i.second.second);
        std::cout << inst_type_to_string(i.first) << ":\t" << i.second.first << " " << i.second.second << std::endl;
    }

    for(auto &i : this->conpo_cntrs){
        // Log("%s:\t%ld", i.first.c_str(), i.second);
        std::cout << i.first << ":\t" << i.second << std::endl;
    }

    for(auto &i : this->cache_cntrs){
        // Log("%s:\t%ld %ld", i.first.c_str(), i.second.first, i.second.second);
        std::cout << i.first << ":\t" << i.second.first << " " << i.second.second << std::endl;
    }
}
