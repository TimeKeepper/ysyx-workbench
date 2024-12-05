#include <performence.hpp>
#include <fstream>

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
        report << i.second.first << std::endl;
        report << i.second.second << std::endl;
    }

    this->inst_cntrs.clear();
    this->conpo_cntrs.clear();
    this->cache_cntrs.clear();
}

void performence::clk_count(){
    if(this->inst_cntrs.empty()) return;

    this->inst_cntrs[Inst_Type::GP].first += 1;
    this->inst_cntrs[this->cur_instType].first += 1;
}

void performence::inst_cont(){
    if(this->inst_cntrs.empty()) return;
    
    this->inst_cntrs[Inst_Type::GP].second += 1;
    this->inst_cntrs[this->cur_instType].second += 1;
}

void performence::inst_type_set(Inst_Type type){
    this->cur_instType = type;
}
