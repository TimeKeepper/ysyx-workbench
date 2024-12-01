#include <muParser.h>
#include <muParserDLL.h>
#include <utils.hpp>

std::string expr(std::string expr) {
    mu::Parser p;

    p.DefineInfixOprt("0x", [](double tar) { 
        return static_cast<double>(std::stoi(std::to_string(static_cast<int>(tar)), nullptr, 16)); 
    });

    p.SetExpr(expr);
    return std::to_string(static_cast<int>(p.Eval()));
}
