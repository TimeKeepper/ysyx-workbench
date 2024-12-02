#ifndef __EXPR_HPP__
#define __EXPR_HPP__

#include <utils.hpp>
#include <vector>
#include <map>

class Expr {
    private:

    typedef enum {
        SPACE,
        HEX,
        DECIMAL,
        EQ,
    } Token_Type;

    // Token map with index
    std::map<Token_Type, uint32_t> token_map = {
        {SPACE, 0},
        {HEX, 1},
        {DECIMAL, 2},
        {EQ, 3},
    };

    class Token {
        public:
        Token_Type type;
        std::string value;

        Token(Token_Type type, const std::string& value) : type(type), value(value) {};
        uint32_t get_val() {
            if(this->type == HEX){
                return std::stoul(this->value, nullptr, 16);
            }
            else if(this->type == DECIMAL){
                return std::stoul(this->value);
            }
            return 0;
        }
    };

    public:
        
        std::vector<Token> get_tokens(std::string expr);
        std::string eval(std::string expr);
};

#endif
