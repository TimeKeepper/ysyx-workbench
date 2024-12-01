#include <muParser.h>
#include <utils.hpp>

std::string expr(std::string expr) {
    mu::Parser p;
    p.SetExpr(expr);
    return std::to_string(p.Eval());
}
