#ifndef __WATCH_POINT_HPP__
#define __WATCH_POINT_HPP__

#include <utils.hpp>
#include <expr.hpp>

class Watch_Point {
    public:
        std::string expr;
        uint32_t value;
        Expr* expr_parser;

        Watch_Point(std::string expr, Expr* expr_parser);
        bool check();
};

class Watch_Point_Manager {
    public:
        std::vector<Watch_Point> watch_points;
        Expr* expr_parser;

        Watch_Point_Manager(Expr* expr_parser);
        bool add_watch_point(std::string expr);
        bool delete_watch_point(uint32_t index);
        uint32_t check_watch_points();

        void print_watch_points();
};

#endif
