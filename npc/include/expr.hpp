#ifndef __EXPR_HPP__
#define __EXPR_HPP__

#include <utils.hpp>
#include <vector>
#include <regex>
#include <queue>
#include <emulator.hpp>

extern Emulator* emulator;

class Expr {
    private:

    enum class TokenType{
        SPACE,

        REGISTER,
        HEX,
        DECIMAL,

        MUL,
        DIV,
        ADD,
        SUB,
        EQ,

        LPAREN,
        RPAREN,
    };

    std::vector<std::pair<TokenType, std::regex>> token_patterns = {
        {TokenType::SPACE, std::regex("\\s+")},
        
        {TokenType::REGISTER, std::regex("\\$[\\$]?[0-9a-zA-Z]+")},
        {TokenType::HEX, std::regex("0[xX][0-9a-fA-F]+")},
        {TokenType::DECIMAL, std::regex("\\d+")},

        {TokenType::MUL, std::regex("\\*")},
        {TokenType::DIV, std::regex("/")},
        {TokenType::ADD, std::regex("\\+")},
        {TokenType::SUB, std::regex("-")},
        {TokenType::EQ, std::regex("==")},

        {TokenType::LPAREN, std::regex("\\(")},
        {TokenType::RPAREN, std::regex("\\)")},
    };

    class Token {
        public:
        TokenType type;
        std::string value;

        Token(TokenType type, const std::string& value) : type(type), value(value) {};
        uint32_t get_val() {
            switch(this->type) {
                int gpr_name2id(const std::string& name);
                case TokenType::REGISTER:   return emulator->cpu.gpr[gpr_name2id(this->value.substr(1))];
                case TokenType::HEX:        return std::stoul(this->value, nullptr, 16);
                case TokenType::DECIMAL:    return std::stoul(this->value);
                default: return 0;
            }
        }
    };

    std::vector<Token> get_tokens(std::string expr);
    std::queue<Expr::Token> RPN(std::vector<Token> tokens);
    int precedence(TokenType type);
    bool expr_valid(std::vector<Token> tokens);

    public:
        
        std::string eval(std::string expr);
        void test();
};

#endif
