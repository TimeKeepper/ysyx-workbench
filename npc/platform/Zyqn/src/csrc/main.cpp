#include <utils.hpp>

void init_monitor(int argc, char *argv[]);
void engine_start(int argc, char **argv);
void exit(void);
int main(int argc, char **argv) {
    init_monitor(argc, argv);

    engine_start(argc, argv);

    exit();
    return 0;
}
