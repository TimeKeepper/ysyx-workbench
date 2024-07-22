import "DPI-C" function void my_putc(input int c);

module UART_bridge(
    input clock,
    input valid,
    input [7:0] data
);

    always @(posedge clock) begin
        if(valid) begin
            my_putc({24'h0, data});
        end
    end

endmodule