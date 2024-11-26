#include <utils.hpp>
#include <emulator.hpp>
#include <simple_debugger.hpp>

void exit(void) {
    Log("Exiting...");
    exit(0);
}

int main(int argc, char **argv) {
    Emulator* emulator = new Emulator(argc, argv);

    simple_debugger* sdb = new simple_debugger(emulator, argc, argv);
    sdb->sdb_mainloop();

    delete emulator;
    delete sdb;
    exit();
    return 0;
}
