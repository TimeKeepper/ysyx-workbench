#[macro_export]
macro_rules! with_lock {
    ($mutex:expr,$lock:ident, $code:block) => {
        let mut $lock =$mutex.lock().unwrap();
        $code
        drop($lock);
    };
    
    ($mutex:expr,$lock:ident, $($mutex_rest:expr, $lock_rest:ident),+,$code:block) => {
        let mut $lock =$mutex.lock().unwrap();
        with_lock!($($mutex_rest, $lock_rest),+,$code);
        drop($lock);
    };
}