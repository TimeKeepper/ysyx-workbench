#ifndef __EXPR_HPP__
#define __EXPR_HPP__

#include <utils.hpp>
#include <vector>
#include <regex>

class Expr {
    private:

    enum class TokenType{
        SPACE,
        HEX,
        DECIMAL,
        EQ,
    };

    std::vector<std::pair<TokenType, std::regex>> token_patterns = {
        {TokenType::SPACE, std::regex("\\s+")},
        {TokenType::HEX, std::regex("0[xX][0-9a-fA-F]+")},
        {TokenType::DECIMAL, std::regex("\\d+")},
        {TokenType::EQ, std::regex("==")}
    };

    class Token {
        public:
        TokenType type;
        std::string value;

        Token(TokenType type, const std::string& value) : type(type), value(value) {};
        uint32_t get_val() {
            if(this->type == TokenType::HEX){
                return std::stoul(this->value, nullptr, 16);
            }
            else if(this->type == TokenType::DECIMAL){
                return std::stoul(this->value);
            }
            return 0;
        }
    };

    std::vector<Token> get_tokens(std::string expr);
    std::vector<Token> RPN(std::vector<Token> tokens);

    public:
        
        std::string eval(std::string expr);
};

#endif
