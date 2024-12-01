#include <muParser.h>
#include <muParserDLL.h>
#include <utils.hpp>

double prefix_hex(double tar) {
    return static_cast<double>(std::stoi(std::to_string(static_cast<int>(tar)), nullptr, 16));
}

std::string expr(std::string expr) {
    mu::Parser p;

    p.DefineFun("0x", prefix_hex);

    p.SetExpr(expr);
    return std::to_string(static_cast<int>(p.Eval()));
}
