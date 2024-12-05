#ifndef __PERFORMENCE_HPP__
#define __PERFORMENCE_HPP__

#include <utils.hpp>
#include <unordered_map>

class performence {
    public:
    enum class Inst_Type{
        GP,
        Cal,
        LS,
        CSR
    };
    performence::Inst_Type cur_instType = Inst_Type::Cal;

    std::unordered_map<Inst_Type, std::pair<uint64_t, uint64_t>> inst_cntrs;
    std::unordered_map<std::string, uint64_t> conpo_cntrs;
    std::unordered_map<std::string, std::pair<uint64_t, uint64_t>> cache_cntrs;

    performence();
    ~performence();

    void clk_count();
    void inst_cont();
    void inst_type_set(Inst_Type type);
};

#endif
