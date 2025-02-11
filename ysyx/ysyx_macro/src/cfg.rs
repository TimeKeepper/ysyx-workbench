#[macro_export]
macro_rules! cfg_wrap {
    ($condition:tt,$($field:tt)*) => {
        $(#[cfg($condition)] $field)*
    };
}