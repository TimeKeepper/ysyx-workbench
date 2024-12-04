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

class Break_Point {
    public:
        uint32_t addr;
        Expr* expr_parser;

        Break_Point(std::string addr_str, Expr* expr_parser);
        bool check();
};

class Watch_Point_Manager {
    public:
        std::vector<Watch_Point> watch_points;
        std::vector<Break_Point> break_points;
        Expr* expr_parser;

        Watch_Point_Manager(Expr* expr_parser);

        bool add_watch_point(std::string expr);
        bool add_break_point(std::string addr);
        bool delete_watch_point(uint32_t index);
        bool delete_break_point(uint32_t index);
        uint32_t check_watch_points();
        uint32_t check_break_points();

        void print_watch_points();
        void print_break_points();
};

#endif
