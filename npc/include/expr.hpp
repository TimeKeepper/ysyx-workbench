#ifndef __EXPR_HPP__
#define __EXPR_HPP__

#include <utils.hpp>
#include <vector>
#include <map>

class Expr {
    private:

    typedef enum {
        SPACE,
        DECIMAL,
        EQ,
    } Token_Type;

    // Token map with index
    std::map<Token_Type, uint32_t> token_map = {
        {SPACE, 0},
        {DECIMAL, 1},
        {EQ, 2},
    };

    struct Token {
        Token_Type type;
        std::string value;

        Token(Token_Type type, const std::string& value) : type(type), value(value) {};
    };

    public:
        std::vector<Token> get_tokens(std::string expr);
        std::string eval(std::string expr);
};

#endif
