#include <utils.hpp>

void init_monitor(int argc, char *argv[]);
void sdb_mainloop(void);
void exit(void);
int main(int argc, char **argv) {
    init_monitor(argc, argv);

    sdb_mainloop();

    exit();
    return 0;
}
