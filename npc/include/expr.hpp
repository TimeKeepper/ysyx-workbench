#ifndef __EXPR_HPP__
#define __EXPR_HPP__

#include <utils.hpp>
#include <vector>
#include <regex>

class Expr {
    private:

    typedef enum {
        HEX,
        DECIMAL,
        EQ,
    } TokenType;

    std::vector<std::pair<TokenType, std::regex>> token_patterns = {
        {HEX, std::regex("0[xX][0-9a-fA-F]+")},
        {DECIMAL, std::regex("\\d+")},
        {EQ, std::regex("==")}
    };

    class Token {
        public:
        TokenType type;
        std::string value;

        Token(TokenType type, const std::string& value) : type(type), value(value) {};
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
