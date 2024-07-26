import "DPI-C" function void inst_comp_update();

module inst_bridge(
    input clock,
    input valid
);

    always @(posedge clock) begin
        if(valid) begin
            inst_comp_update();
        end
    end

endmodule